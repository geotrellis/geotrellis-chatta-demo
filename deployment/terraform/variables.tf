variable "environment" {
  default = "Production"
}

variable "remote_state_bucket" {
  type        = "string"
  description = "Core infrastructure config bucket"
}

variable "aws_account_id" {
  default     = "896538046175"
  description = "Geotrellis Chatta account ID"
}

variable "aws_region" {
  default = "us-east-1"
}

variable "image_version" {
  type        = "string"
  description = "Geotrellis Chatta Image version"
}

variable "cdn_price_class" {
  default = "PriceClass_200"
}

variable "ecs_autoscaling_role_name" {
  default = "AWSServiceRoleForApplicationAutoScaling_ECSService"
}

variable "ssl_certificate_arn" {}

variable "chatta_ecs_desired_count" {
  default = "1"
}

variable "chatta_ecs_min_count" {
  default = "1"
}

variable "chatta_ecs_max_count" {
  default = "2"
}

variable "chatta_ecs_deployment_min_percent" {
  default = "100"
}

variable "chatta_ecs_deployment_max_percent" {
  default = "200"
}
