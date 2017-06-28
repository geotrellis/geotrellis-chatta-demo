#
# Website ALB security group resources
#
resource "aws_security_group_rule" "alb_chatta_https_ingress" {
  type        = "ingress"
  from_port   = 443
  to_port     = 443
  protocol    = "tcp"
  cidr_blocks = ["0.0.0.0/0"]

  security_group_id = "${module.chatta_ecs_service.lb_security_group_id}"
}

resource "aws_security_group_rule" "alb_chatta_container_instance_all_egress" {
  type      = "egress"
  from_port = 0
  to_port   = 65535
  protocol  = "tcp"

  security_group_id        = "${module.chatta_ecs_service.lb_security_group_id}"
  source_security_group_id = "${data.terraform_remote_state.core.container_instance_security_group_id}"
}

#
# Container instance security group resources
#
resource "aws_security_group_rule" "container_instance_alb_chatta_all_ingress" {
  type      = "ingress"
  from_port = 0
  to_port   = 65535
  protocol  = "tcp"

  security_group_id        = "${data.terraform_remote_state.core.container_instance_security_group_id}"
  source_security_group_id = "${module.chatta_ecs_service.lb_security_group_id}"
}

resource "aws_security_group_rule" "container_instance_alb_chatta_all_egress" {
  type      = "egress"
  from_port = 0
  to_port   = 65535
  protocol  = "tcp"

  security_group_id        = "${data.terraform_remote_state.core.container_instance_security_group_id}"
  source_security_group_id = "${module.chatta_ecs_service.lb_security_group_id}"
}
