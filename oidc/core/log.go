package core

import (
	"context"
	"github.com/rs/zerolog"
	"go.opentelemetry.io/otel"
	"os"
)

var tracer = otel.Tracer("example-tracer")

func main() {
	// 设置 zerolog 输出到标准输出
	log := zerolog.New(os.Stdout).With().Timestamp().Logger()

	// 创建 OpenTelemetry Span
	_, span := tracer.Start(context.Background(), "example-span")
	defer span.End()

	// 将追踪信息嵌入到日志中
	log.Info().
		Str("trace_id", span.SpanContext().TraceID().String()).
		Str("span_id", span.SpanContext().SpanID().String()).
		Msg("This log is part of a trace")
}
