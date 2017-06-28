data "terraform_remote_state" "core" {
  backend = "s3"

  config {
    region = "${var.aws_region}"
    bucket = "${var.remote_state_bucket}"
    key    = "terraform/core/state"
  }
}

data "aws_route53_zone" "external" {
  zone_id = "${data.terraform_remote_state.core.public_hosted_zone_id}"
}
