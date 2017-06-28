#
# ECS Resources
#

# Template for container definition, allows us to inject environment
data "template_file" "ecs_chatta_task" {
  template = "${file("${path.module}/task-definitions/chatta.json")}"

  vars {
    chatta_image       = "${var.aws_account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/gt-chatta:${var.image_version}"
    chatta_region      = "${var.aws_region}"
    chatta_environment = "${var.environment}"
  }
}

# Allows resource sharing among multiple containers
resource "aws_ecs_task_definition" "chatta" {
  family                = "${var.environment}Chatta"
  container_definitions = "${data.template_file.ecs_chatta_task.rendered}"
}

resource "aws_cloudwatch_log_group" "chatta" {
  name = "log${var.environment}ChattaDemo"

  tags {
    Environment = "${var.environment}"
  }
}

module "chatta_ecs_service" {
  source = "github.com/azavea/terraform-aws-ecs-web-service?ref=0.2.0"

  name                = "Chatta"
  vpc_id              = "${data.terraform_remote_state.core.vpc_id}"
  public_subnet_ids   = ["${data.terraform_remote_state.core.public_subnet_ids}"]
  access_log_bucket   = "${data.terraform_remote_state.core.logs_bucket_id}"
  access_log_prefix   = "ALB/Chatta"
  port                = "8777"
  ssl_certificate_arn = "${var.ssl_certificate_arn}"

  cluster_name                   = "${data.terraform_remote_state.core.container_instance_name}"
  task_definition_id             = "${aws_ecs_task_definition.chatta.family}:${aws_ecs_task_definition.chatta.revision}"
  desired_count                  = "${var.chatta_ecs_desired_count}"
  min_count                      = "${var.chatta_ecs_min_count}"
  max_count                      = "${var.chatta_ecs_max_count}"
  deployment_min_healthy_percent = "${var.chatta_ecs_deployment_min_percent}"
  deployment_max_percent         = "${var.chatta_ecs_deployment_max_percent}"
  container_name                 = "gt-chatta"
  container_port                 = "8777"
  ecs_service_role_name          = "${data.terraform_remote_state.core.ecs_service_role_name}"
  ecs_autoscale_role_arn         = "${data.terraform_remote_state.core.ecs_autoscale_role_arn}"

  project     = "Geotrellis Chatta"
  environment = "${var.environment}"
}
