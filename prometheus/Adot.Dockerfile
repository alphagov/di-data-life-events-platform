FROM public.ecr.aws/aws-observability/aws-otel-collector:latest
COPY adot-config.template.yaml /etc/ecs/adot-config.template.yaml
COPY start_collector.sh /etc/ecs/start_collector.sh
CMD ["--config=/etc/ecs/adot-config.yaml"]
ENTRYPOINT ["/etc/ecs/start_collector.sh"]
