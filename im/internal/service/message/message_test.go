package message

import (
	"context"
	"im/db"
	"im/ent/channel"
	"im/ent/schema"
	"im/pkg/logger"
	"testing"
)

func TestSendMessage(t *testing.T) {
	server := NewMessageServer(db.Ctx())

	err := server.SendMessage(context.Background(), &SendMessageReq{Content: schema.Content{
		Msg: "sadad",
	}, MemberToId: 1, MemberFromId: 2, ConversationId: 4, BizType: channel.TypeConversation})
	if err != nil {
		logger.Fatal(context.Background()).Err(err).Msg("send message fail")
	}
}

func TestMessageServer_GetMessageNotify(t *testing.T) {
	server := NewMessageServer(db.Ctx())
	id, err := server.GetMessageNotify(context.Background(), &QueryMessageReq{
		MemberId:   1,
		ChannelIds: []int{4},
	})
	if err != nil {
		logger.Fatal(context.Background()).Err(err)
	}
	logger.Info(context.Background()).Any("message", id)

}

func TestGetNewMessageByMemberId(t *testing.T) {
	server := NewMessageServer(db.Ctx())
	id, err := server.GetNewMessageByMemberId(context.Background(), &QueryMessageReq{
		MemberId:   1,
		ChannelIds: []int{4},
	})
	if err != nil {
		logger.Fatal(context.Background()).Err(err)
	}
	logger.Info(context.Background()).Any("message", id)

}
func BenchmarkSendMessage(b *testing.B) {
	server := NewMessageServer(db.Ctx())
	for i := 0; i < b.N; i++ {
		err := server.SendMessage(context.Background(), &SendMessageReq{Content: schema.Content{
			Msg: "sadad",
		}, MemberToId: 1, MemberFromId: 2, ConversationId: 4, BizType: channel.TypeConversation})
		if err != nil {
			return
		}
	}
}
