package mco.io

import mco.general._
import mco.io.files.Path
import mco.io.files.ops._
import cats.data.{Xor, XorT}
import cats.instances.stream._
import cats.syntax.applicative._
import cats.syntax.functor._


class IsolatedRepo private (source: Source[IO], target: Path, known: Map[String, (Package, Media[IO])]) extends Repository[IO, Path] {
  import IsolatedRepo.{Repo, XorTIO}

  override type State = Unit
  override def state: Unit = ()

  override def apply(key: String): Package = known(key)._1
  override lazy val packages: Traversable[Package] = known.values.map{ case (pkg, _) => pkg }
  override def change(oldKey: String, upd: Package): IO[Repo] = {
    def onChange(body: IsolatedRepo => Boolean)(action: IsolatedRepo => IO[IsolatedRepo]) =
      (s: IsolatedRepo) => if (body(s)) action(s) else IO pure s

    val key = upd.key

    val result: IO[IsolatedRepo] = { this.pure[IO] } flatMap
      onChange(_ => oldKey != key)(_ rename (oldKey, key)) flatMap
      onChange(_(key).contents != upd.contents)(_ reselect upd) flatMap
      onChange(r => r(key).isInstalled != upd.isInstalled) {
        r => if (upd.isInstalled) r install key else r uninstall key
      }

    result.widen
  }

  def rename(oldKey: String, newKey: String): IO[IsolatedRepo] = for {
    _ <- moveTree(target / oldKey, target / newKey)
    newSource <- source.rename(oldKey, newKey)
    (pkg, _) = known(oldKey)
    newMedias <- newSource.list
    media = newMedias.collect { case (_, m) if m.key == newKey => m } .head
    newKnown = known - oldKey + { (newKey, (pkg.copy(key = newKey), media)) }
  } yield new IsolatedRepo(newSource, target, newKnown)

  private def reselect(upd: Package) = {
    val nextContents = upd.contents
    require(nextContents.map(_.key) == apply(upd.key).contents.map(_.key))

    def updated(r: IsolatedRepo) = {
      val (pkg, media) = known(upd.key)
      val newKnown = known.updated(upd.key, (pkg.copy(contents = nextContents), media))
      new IsolatedRepo(source, target, newKnown).pure[IO]
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

      _ <- media.copy(installable.toMap)
      nextPkg = pkg.copy(contents = nextContents, isInstalled = true)
      nextKnown = known.updated(pkg.key, (nextPkg, media))
    } yield new IsolatedRepo(source, target, nextKnown)
  }

  private def uninstall(key: String) = {
    val targetDir = target / key
    val (pkg, media) = known(key)
    val nextContents = for (content <- pkg.contents) yield content.copy(isInstalled = false)
    for {
      _ <- removeFile(targetDir)
      nextPkg = pkg.copy(contents = nextContents, isInstalled = false)
      nextKnown = known.updated(pkg.key, (nextPkg, media))
    } yield new IsolatedRepo(source, target, nextKnown)
  }

  override def add(f: String): IO[Xor[String, Repo]] = {
    for {
      nextSource <- XorTIO(source add f)
      nextElements <- XorTIO.liftT(nextSource.list)
      Some(t) = nextElements find { case (pkg, _) => pkg.key == f }
    } yield new IsolatedRepo (nextSource, target, known + (f -> t)): Repo
  }.value

  override def remove(s: String): IO[Xor[String, Repo]] = {
    val pkg = apply(s)
    if (pkg.isInstalled) change(s, pkg.copy(isInstalled = false)) flatMap {_.remove(s)}
    else XorTIO(source remove s)
      .map(nextSource => new IsolatedRepo(nextSource, target, known - s) : Repo)
      .value
  }
}

object IsolatedRepo extends Repository.Companion[IO, Path, Unit] {
  private type Repo = Repository.Aux[IO, Path, Unit]
  override def apply(source: Source[IO], target: Path, state: Unit): IO[Repo] =
    for {
      packages <- source.list
      updated <- IO.traverse(packages)({statusFromExisting(target) _}.tupled)
    } yield new IsolatedRepo(source, target, updated.toMap)

  private def statusFromExisting(target: Path)(pkg: Package, media: Media[IO]) = for {
    exists <- isDirectory(target / pkg.key)
    fileExists <- if (exists) media.readContent else ((_: String) => false).pure[IO]
    updatedContent = pkg.contents.map(c => c.copy(isInstalled = fileExists(c.key)))
  } yield pkg.key -> (pkg.copy(contents = updatedContent, isInstalled = exists) -> media)

  type XorTIO[A] = XorT[IO, String, A]
  object XorTIO {
    def apply[A](x: IO[String Xor A]): XorT[IO, String, A] = XorT(x)
    def liftT[A](x: IO[A]): XorT[IO, String, A] = XorT.liftT(x)
  }
}