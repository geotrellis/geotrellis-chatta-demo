# Amazon Web Services Deployment

Amazon Web Services deployment is driven by [Terraform](https://terraform.io/) and the [AWS Command Line Interface (CLI)](http://aws.amazon.com/cli/).

## Table of Contents

* [AWS Credentials](#aws-credentials)
* [Terraform](#terraform)

## AWS Credentials

Using the AWS CLI, create an AWS profile named `geotrellis`:

```bash
$ aws --profile geotrellis configure
AWS Access Key ID [********************]:
AWS Secret Access Key [********************]:
Default region name [us-east-1]: us-east-1
Default output format [None]:
```

You will be prompted to enter your AWS credentials, along with a default region. These credentials will be used to authenticate calls to the AWS API when using Terraform and the AWS CLI.

## Terraform

Next, use the `infra` wrapper script to lookup the remote state of the infrastructure and assemble a plan for work to be done:

```bash
$ export GT_CHATTA_SETTINGS_BUCKET="geotrellis-site-production-config-us-east-1"
$ export AWS_PROFILE="geotrellis"
# TRAVIS_COMMIT is the 7-digit SHA for the commit you want to deploy
$ export TRAVIS_COMMIT=1a3b5c7
$ docker-compose -f docker-compose.ci.yml run --rm terraform ./scripts/infra.sh plan
```

Once the plan has been assembled, and you agree with the changes, apply it:

```bash
$ docker-compose -f docker-compose.ci.yml run --rm terraform ./scripts/infra.sh apply
```

This will attempt to apply the plan assembled in the previous step using Amazon's APIs. In order to change specific attributes of the infrastructure, inspect the contents of the environment's configuration file in Amazon S3.
