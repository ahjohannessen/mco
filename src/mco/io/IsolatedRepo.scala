package mco.io

import mco.general._
import Files._
import cats.data.{Xor, XorT}


class IsolatedRepo private (source: Source[IO], target: Path, known: Map[String, (Package, Media[IO])]) extends Repository[IO, Path] {
  import IsolatedRepo.{Repo, XorTIO}

  override type State = Unit
  override def state = ()

  override def apply(key: String) = known(key)._1
  override lazy val packages = known.values.map{ case (pkg, _) => pkg }
  override def change(oldKey: String, upd: Package): IO[Repo] = {
    def onChange(body: IsolatedRepo => Boolean)(action: IsolatedRepo => IO[IsolatedRepo]) =
      (s: IsolatedRepo) => if (body(s)) action(s) else IO pure s

    val key = upd.key

    val result: IO[IsolatedRepo] = { IO pure this } flatMap
      onChange(_ => oldKey != key)(_ rename (oldKey, key)) flatMap
      onChange(_(key).contents != upd.contents)(_ reselect upd) flatMap
      onChange(r => r(key).isInstalled != upd.isInstalled) {
        r => if (upd.isInstalled) r install key else r uninstall key
      }

    IO widen result
  }

  def rename(oldKey: String, newKey: String): IO[IsolatedRepo] = for {
    _ <- move(target / oldKey, target / newKey)
    newSource <- source.rename(oldKey, newKey)
    newKnown = known - oldKey + (newKey -> known(newKey))
  } yield new IsolatedRepo(newSource, target, newKnown)

  def reselect(upd: Package) = {
    val nextContents = upd.contents
    require(nextContents.map(_.key) == apply(upd.key).contents.map(_.key))

    def updated(r: IsolatedRepo) = {
      val (pkg, media) = known(upd.key)
      val newKnown = known.updated(upd.key, (pkg.copy(contents = nextContents), media))
      IO.pure(new IsolatedRepo(source, target, newKnown))
    }

    if (apply(upd.key).isInstalled) for {
      a <- this.uninstall(upd.key)
      b <- updated(a)
      c <- b.install(upd.key)
    } yield c
    else updated(this)
  }

  def install(key: String) = {
    val targetDir = target / key
    val (pkg, media) = known(key)
    val nextContents = for (content <- pkg.contents) yield
      content.copy(isInstalled = content.kind.isInstallable)
    for {
      _ <- removeFile(targetDir)
      _ <- createDirectory(targetDir)

      installable = nextContents.filter(_.isInstalled).map(c => c.key -> (targetDir / c.key).asString)

      _ <- media.copy(installable.toMap)
      nextPkg = pkg.copy(contents = nextContents, isInstalled = true)
      nextKnown = known.updated(pkg.key, (nextPkg, media))
    } yield new IsolatedRepo(source, target, nextKnown)
  }

  def uninstall(key: String) = {
    val targetDir = target / key
    val (pkg, media) = known(key)
    val nextContents = for (content <- pkg.contents) yield content.copy(isInstalled = false)
    for {
      _ <- removeFile(targetDir)
      nextPkg = pkg.copy(contents = nextContents, isInstalled = false)
      nextKnown = known.updated(pkg.key, (nextPkg, media))
    } yield new IsolatedRepo(source, target, nextKnown)
  }

  override def add(f: String) = {
    for {
      nextSource <- XorTIO(source add f)
      nextElements <- XorTIO.liftT(nextSource.list)
      Some(t) = nextElements find { case (pkg, _) => pkg.key == f }
    } yield new IsolatedRepo (nextSource, target, known + (f -> t)): Repo
  }.value

  override def remove(s: String) = {
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
      updated <- IO.sequence(packages map {statusFromExisting(target) _}.tupled)
    } yield new IsolatedRepo(source, target, updated.toMap)

  private def statusFromExisting(target: Path)(pkg: Package, media: Media[IO]) = for {
    exists <- isDirectory(target / pkg.key)
    fileExists <- if (exists) media.readContent else IO.pure((_: String) => false)
    updatedContent = pkg.contents.map(c => c.copy(isInstalled = fileExists(c.key)))
  } yield pkg.key -> (pkg.copy(contents = updatedContent, isInstalled = exists) -> media)

  type XorTIO[A] = XorT[IO, String, A]
  object XorTIO {
    def apply[A](x: IO[String Xor A]) = XorT[IO, String, A](x)
    def liftT[A](x: IO[A]) = XorT.liftT[IO, String, A](x)
  }
}