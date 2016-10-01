package mco.config

case class RepoConfig
(
  key: String,
  kind: RepoKind.Value,
  source: String,
  target: String,
  classifier: String,
  media: Vector[String],
  persistency: Persistency.Value
)
