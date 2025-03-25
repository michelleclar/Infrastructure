package service

import (
	"context"
	"im/db"
	channel2 "im/ent/channel"
	"im/ent/schema"
	"im/internal/service/channel"
	"im/internal/service/message"
	"im/pkg/logger"
	"testing"
)

func Case1() {
	ctx := db.Ctx()
	// NOTE: create conversation
	channelServer := channel.NewChannelServer(ctx)
	err := channelServer.CreatChannel(context.Background(), &channel.CreateChannelReq{MemberFromId: 1, MemberToId: 2, ChannelType: channel2.TypeConversation})
	if err != nil {
		logger.Fatal(context.Background()).Err(err).Msg("create channel fail")
	}

	messageServer := message.NewMessageServer(ctx)
	// NOTE: send message
	err = messageServer.SendMessage(context.Background(), &message.SendMessageReq{Content: schema.Content{
		Msg: "sadad",
	}, MemberToId: 1, MemberFromId: 2, ChannelId: 4})
	if err != nil {
		logger.Fatal(context.Background()).Err(err).Msg("send message fail")
	}

	id, err := messageServer.GetMessageNotify(context.Background(), &message.QueryMessageReq{
		MemberId:   1,
		ChannelIds: []int{4},
	})
	if err != nil {
		logger.Fatal(context.Background()).Err(err).Msg("get notify fail")
	}
	logger.Info(context.Background()).Any("message", id)
}
func Case2() {
	ctx := db.Ctx()
	messageServer := message.NewMessageServer(ctx)
	// NOTE: show message total
	total, err := messageServer.ShowMessageTotal(context.Background(), &message.QueryMessageReq{
		MemberId: 1,
	})
	if err != nil {
		logger.Fatal(context.Background()).Err(err).Msg("show message fail")
	}
	logger.Info(context.Background()).Any("message total", total)
	// NOTE: show message detail
	notify, err := messageServer.GetMessageNotify(context.Background(), &message.QueryMessageReq{
		MemberId:   1,
		ChannelIds: []int{4},
	})
	if err != nil {
		logger.Fatal(context.Background()).Err(err).Msg("get notify fail")
	}
	logger.Info(context.Background()).Any("notify", notify)

}

func TestCase1(t *testing.T) {
	Case1()
}
func TestCase2(t *testing.T) {
	Case2()
}
func BenchmarkCase1(b *testing.B) {
	for i := 0; i < b.N; i++ {
		Case1()
	}
}
