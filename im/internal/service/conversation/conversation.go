package conversation

import (
	"im/db"
	"im/ent"
)

type ConversationServer struct {
	conversationClient   ent.ConversationsClient
	conversationMessages ent.ConversationMessagesClient
	db                   db.Context
}

func NewConversationServer(db db.Context) *ConversationServer {
	return &ConversationServer{db: db,
		conversationClient: *db.GetClient().Conversations,
	}
}

func (server ConversationServer) getConversationItemsByMemberId(memberId int) {
	//server.conversationClient.Query().Where(conversations.Member(memberId))

}
