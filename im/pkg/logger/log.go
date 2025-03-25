package logger

import (
	"context"
	"github.com/cloudwego/hertz/pkg/common/hlog"
	hertzlogrus "github.com/hertz-contrib/obs-opentelemetry/logging/logrus"
	"github.com/hertz-contrib/obs-opentelemetry/provider"
	"github.com/rs/zerolog"
	"go.opentelemetry.io/otel/trace"
	"os"
)

// TODO: need share context by goroutine
var log zerolog.Logger

func init() {
	//tracer, config := hertztracing.NewServerTracer()
	hlog.SetLogger(hertzlogrus.NewLogger())
	hlog.SetLevel(hlog.LevelDebug)
	log = zerolog.New(os.Stdout).With().Timestamp().Logger()
	//TODO: reade by env
	zerolog.SetGlobalLevel(zerolog.DebugLevel)
}

type Logger struct {
	*hertzlogrus.Logger
}

func NewLogger(serverName string) Logger {
	hlog.SetLogger(hertzlogrus.NewLogger())
	hlog.SetLevel(hlog.LevelDebug)
	provider.NewOpenTelemetryProvider(provider.WithServiceName(serverName),
		//TODO: Support setting ExportEndpoint via environment variables: OTEL_EXPORTER_OTLP_ENDPOINT
		provider.WithExportEndpoint("localhost:4317"),
		provider.WithInsecure())
	return Logger{}
}

func Info(ctx context.Context) *zerolog.Event {
	span := trace.SpanFromContext(ctx)
	event := log.Info()
	if span.SpanContext().IsValid() {
		event = event.Str("traceID", span.SpanContext().TraceID().String()).Str("spanID", span.SpanContext().SpanID().String())
	}
	return event
}
func Trace(ctx context.Context) *zerolog.Event {
	span := trace.SpanFromContext(ctx)
	event := log.Trace()
	log.Fatal()
	if span.SpanContext().IsValid() {
		event = event.Str("traceID", span.SpanContext().TraceID().String()).Str("spanID", span.SpanContext().SpanID().String())
	}
	return event
}
func Fatal(ctx context.Context) *zerolog.Event {
	span := trace.SpanFromContext(ctx)
	event := log.Fatal()
	if span.SpanContext().IsValid() {
		event = event.Str("traceID", span.SpanContext().TraceID().String()).Str("spanID", span.SpanContext().SpanID().String())
	}
	return event
}
func Debug(ctx context.Context) *zerolog.Event {
	span := trace.SpanFromContext(ctx)
	event := log.Debug()
	if span.SpanContext().IsValid() {
		event = event.Str("traceID", span.SpanContext().TraceID().String()).Str("spanID", span.SpanContext().SpanID().String())
	}
	return event
}
func Error(ctx context.Context) *zerolog.Event {
	span := trace.SpanFromContext(ctx)
	event := log.Error()
	if span.SpanContext().IsValid() {
		event = event.Str("traceID", span.SpanContext().TraceID().String()).Str("spanID", span.SpanContext().SpanID().String())
	}
	return event
}

func Warn(ctx context.Context) *zerolog.Event {
	span := trace.SpanFromContext(ctx)
	event := log.Warn()
	if span.SpanContext().IsValid() {
		event = event.Str("traceID", span.SpanContext().TraceID().String()).Str("spanID", span.SpanContext().SpanID().String())
	}
	return event
}
