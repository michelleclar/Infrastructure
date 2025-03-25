package channel

import (
	"context"
	"im/db"
	"im/ent/channel"
	"log"
	"testing"
)

func getServer() *ChannelServer {
	return NewChannelServer(db.Ctx())
}

func TestCreateChannelByMemberId(t *testing.T) {
	server := getServer()
	flag := server.CreatChannel(context.Background(), &CreateChannelReq{
		MemberFromId: 1,
		MemberToId:   2,
		ChannelType:  channel.TypeConversation,
	})
	log.Println(flag)
}

func TestChannelItemsByMemberId(t *testing.T) {
	//ctx := context.Background()
	//if err := Ctx().Schema.Create(ctx); err != nil {
	//	logger.Fatal("failed creating schema resources", err)
	//}
	server := getServer()
	server.GetChannelItems(context.Background(), &QueryChannelReq{MemberId: 1})
}
