package mco.core.general

import scala.language.higherKinds

import mco.core.monad.IO


class Repository(target: Target[IO], packages: Packages[IO]) {
  def all = packages.all
  def apply(key: String) = packages(key)

  def install(key: String): IO[Repository] = for {
    ctx <- target output key
    pkgs <- packages install (key, ctx)
  } yield new Repository(target, pkgs)

  def uninstall(key: String): IO[Repository] = for {
    ctx <- target output key
    pkgs <- packages uninstall (key, ctx)
  } yield new Repository(target, pkgs)
}

