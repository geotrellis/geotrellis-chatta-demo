resource "aws_cloudfront_distribution" "cdn" {
  origin {
    domain_name = "${aws_route53_record.origin.fqdn}"
    origin_id   = "originChattaSite"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1", "TLSv1.1", "TLSv1.2"]
    }
  }

  enabled          = true
  http_version     = "http2"
  comment          = "GeoTrellis Chattanooga Demo (${var.environment})"
  retain_on_delete = true

  price_class = "${var.cdn_price_class}"

  # The trailing period at the end of the name is stripped off to comply with CloudFront's CNAME policy.
  aliases = ["chatta.${replace(data.aws_route53_zone.external.name, "/.$/", "")}"]

  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD", "OPTIONS"]
    target_origin_id = "originChattaSite"

    forwarded_values {
      query_string = true
      headers      = ["*"]

      cookies {
        forward = "all"
      }
    }

    compress               = false
    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 0
    max_ttl                = 300
  }

  logging_config {
    include_cookies = false
    bucket          = "${data.terraform_remote_state.core.logs_bucket_id}.s3.amazonaws.com"
    prefix          = "CloudFront/Chatta/"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = "${var.ssl_certificate_arn}"
    minimum_protocol_version = "TLSv1"
    ssl_support_method       = "sni-only"
  }
}
