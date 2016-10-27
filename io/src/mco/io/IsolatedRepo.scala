package mco.io

import scala.collection.immutable.SortedSet

import cats.instances.stream._
import cats.syntax.applicative._
import cats.syntax.functor._
import mco._
import mco.io.files._


class IsolatedRepo private (val key: String, source: Source[IO], target: Path, known: Map[String, (Package, Media[IO])]) extends Repository[IO, Set[Package]] {

  override def state: Set[Package] = packages

  override def apply(key: String): Package = known(key)._1

  override lazy val packages: SortedSet[Package] =
    known.values.map {case (pkg, _) => pkg } (collection.breakOut)

  override def change(oldKey: String, upd: Package): IO[Self] = {
    def onChange(body: IsolatedRepo => Boolean)(action: IsolatedRepo => IO[IsolatedRepo]) =
      (s: IsolatedRepo) => if (body(s)) action(s) else s.pure[IO]

    val key = upd.key

    val result: IO[IsolatedRepo] = { this.pure[IO] } flatMap
      onChange(_ => oldKey != key)(_ rename (oldKey, key)) flatMap
      onChange(_(key).contents != upd.contents)(_ reselect upd) flatMap
      onChange(r => r(key).isInstalled != upd.isInstalled) {
        r => if (upd.isInstalled) r install key else r uninstall key
      }

    result.widen
  }

  private def rename(oldKey: String, newKey: String): IO[IsolatedRepo] = for {
    _ <- moveTree(target / oldKey, target / newKey)
    result <- source.rename(oldKey, newKey)
    (newSource, media) = result
    (pkg, _) = known(oldKey)
    newKnown = known - oldKey + { (newKey, (pkg.copy(key = newKey), media)) }
  } yield new IsolatedRepo(key, newSource, target, newKnown)

  private def reselect(upd: Package) = {
    val nextContents = upd.contents
    require(nextContents.map(_.key) == apply(upd.key).contents.map(_.key))

    def updated(r: IsolatedRepo) = {
      val (pkg, media) = known(upd.key)
      val newKnown = known.updated(upd.key, (pkg.copy(contents = nextContents), media))
      new IsolatedRepo(key, source, target, newKnown).pure[IO]
    }

    if (apply(upd.key).isInstalled) for {
      a <- this.uninstall(upd.key)
      b <- updated(a)
      c <- b.install(upd.key)
    } yield c
    else updated(this)
  }

  private def install(key: String) = {
    val targetDir = target / key
    val (pkg, media) = known(key)
    val nextContents = for (content <- pkg.contents) yield
      content.copy(isInstalled = content.kind.isInstallable)
    for {
      _ <- removeFile(targetDir)
      _ <- createDirectory(targetDir)

      installable = nextContents
        .filter(_.isInstalled)
        .map(c => c.key -> (targetDir / c.key).asString)

      _ <- media copy installable.toMap
    } yield withUpdated(pkg.copy(contents = nextContents, isInstalled = true), media)
  }

  private def uninstall(key: String) = {
    val targetDir = target / key
    val (pkg, media) = known(key)
    val nextContents = for (content <- pkg.contents) yield content.copy(isInstalled = false)
    for {
      _ <- removeFile(targetDir)
    } yield withUpdated(pkg.copy(contents = nextContents, isInstalled = false), media)
  }

  private def withUpdated(pkg: Package, media: Media[IO]) ={
    val nextKnown = known.updated(pkg.key, (pkg, media))
    new IsolatedRepo(key, source, target, nextKnown)
  }

  override def add(f: String): IO[Self] = {
    for {
      nextSource <- source add f
      nextElements <- nextSource.list
      fileName = Path(f).fileName
      Some(t) = nextElements find { case (pkg, _) => pkg.key == fileName }
    } yield new IsolatedRepo(key, nextSource, target, known + (fileName -> t)).widen
  }

  override def remove(s: String): IO[Self] = {
    val pkg = apply(s)
    if (pkg.isInstalled) change(s, pkg.copy(isInstalled = false)) flatMap {_.remove(s)}
    else (source remove s)
      .map(nextSource => new IsolatedRepo(key, nextSource, target, known - s).widen)
  }
}

object IsolatedRepo extends Repository.Companion[IO, Set[Package]] {
  override def apply(key: String, source: Source[IO], target: String, state: Set[Package]): IO[Repository[IO, Set[Package]]] =
    for {
      existing <- source.list
      updated <- IO.traverse(existing)({statusFromExisting(Path(target)) _}.tupled)
      known = state.map(p => p.key -> p).toMap
      repos = updated.map {
        case (pkey, (pkg, media)) if known contains pkey =>
          (pkey, (known(pkey).copy(isInstalled = pkg.isInstalled), media))
        case tuple: Any => tuple
      }
    } yield new IsolatedRepo(key, source, Path(target), repos.toMap): Repository[IO, Set[Package]]

  private def statusFromExisting(target: Path)(pkg: Package, media: Media[IO]) = for {
    exists <- isDirectory(target / pkg.key)
    fileExists <- if (exists) media.readContent else ((_: String) => false).pure[IO]
    updatedContent = pkg.contents.map(c => c.copy(isInstalled = fileExists(c.key)))
  } yield pkg.key -> (pkg.copy(contents = updatedContent, isInstalled = exists) -> media)
}
