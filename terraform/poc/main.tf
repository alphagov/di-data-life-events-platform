terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
  backend "s3" {
    bucket         = "gdx-data-share-poc-tfstate"
    key            = "terraform-poc.tfstate"
    region         = "eu-west-2"
    dynamodb_table = "gdx-data-share-poc-lock"
    encrypt        = true
  }
}

provider "aws" {
  region = "eu-west-2"
  default_tags {
    tags = {
      source      = "terraform"
      repository  = "https://github.com/alphagov/gdx-data-share-poc"
      environment = "poc"
    }
  }
}

module "ecr" {
  source = "../modules/ecr"
}