package log

import (
	"context"
	"github.com/rs/zerolog"
	"go.opentelemetry.io/otel/trace"
	"os"
)

// TODO: need share context by goroutine
var log zerolog.Logger

func init() {
	log = zerolog.New(os.Stdout).With().Timestamp().Logger()
	//TODO: reade by env
	zerolog.SetGlobalLevel(zerolog.InfoLevel)
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
