package message

import (
	"context"
	"errors"
	"im/db"
	"im/ent"
	"im/ent/channel"
	"im/ent/messagebox"
	"im/ent/schema"
	channel2 "im/internal/service/channel"
	"im/pkg/logger"
)

type MessageServer struct {
	messageClient        ent.MessagesClient
	messageHistoryClient ent.MessageHistoryClient
	messageBoxClient     ent.MessageBoxClient
	messageStatusClient  ent.MessageStatusClient
	db                   db.Context
}

type QueryMessageReq struct {
	MemberId   int
	ChannelIds []int
}

type SendMessageReq struct {
	MemberFromId   int            `json:"memberFromId"`
	MemberToId     int            `json:"memberToId"`
	BizType        channel.Type   `json:"channelType"`
	ChannelId      int            `json:"channelId"`
	ConversationId int            `json:"conversationId"`
	Content        schema.Content `json:"content"`
}

type MessageNotify struct {
	MemberId  int `json:"member_id,omitempty,memberId"`
	ChannelId int `json:"channel_id,omitempty,channelId"`
	Count     int `json:"count,omitempty"`
}

func NewMessageServer(db db.Context) *MessageServer {
	return &MessageServer{db: db,
		messageClient:        *db.GetClient().Messages,
		messageBoxClient:     *db.GetClient().MessageBox,
		messageHistoryClient: *db.GetClient().MessageHistory,
		messageStatusClient:  *db.GetClient().MessageStatus,
	}
}

func (msg *MessageServer) SendMessage(ctx context.Context, req *SendMessageReq) error {
	if req.BizType == "" {
		return errors.New("biz type is required")
	}
	return msg.db.WithTx(ctx, func(tx *ent.Tx) error {
		message, err := tx.Messages.Create().SetContext(req.Content).Save(ctx)
		if err != nil {
			logger.Error(ctx).Err(err).Msg("Failed to save message")
			return err
		}
		logger.Debug(ctx).Msgf("created message success: %v", message)
		switch req.BizType {
		case channel.TypeConversation:
			{
				//TODO:check channel exist

				conversationMessages, err := tx.ConversationMessages.Create().
					SetMessageID(message.ID).
					SetConversationID(req.ConversationId).
					SetMemberFromID(req.MemberFromId).
					SetMemberToID(req.MemberToId).
					Save(ctx)
				if err != nil {
					logger.Error(ctx).Err(err).Msg("Failed to save conversation messages")
					return err
				}
				logger.Debug(ctx).Msgf("conversation success: %v", conversationMessages)
			}
		default:
			{
				return errors.New(string("not supported biz type" + req.BizType))
			}
		}

		//messageBox, err := tx.MessageBox.Create().SetChannelID(req.ChannelId).SetMemberID(req.MemberToId).SetMessageID(message.ID).Save(ctx)
		//if err != nil {
		//	logger.Error(ctx).Err(err).Msg("Failed to save message box")
		//	return err
		//}
		//logger.Debug(ctx).Msgf("created message box success: %v", messageBox)
		return nil
	})
}

func (msg *MessageServer) GetMessageNotify(ctx context.Context, req *QueryMessageReq) ([]MessageNotify, error) {
	resp := []MessageNotify{}
	items, err := channel2.NewChannelServer(msg.db).GetChannelItems(ctx, &channel2.QueryChannelReq{
		MemberId: req.MemberId,
	})
	if err != nil {
		return nil, err
	}

	if len(items) == 0 {
		return nil, errors.New("no items found")
	}
	m := make(map[channel.Type][]int)
	for _, item := range items {
		if item.ChannelType == channel.TypeConversation {
			m[item.ChannelType] = append(m[item.ChannelType], item.ChannelId)
			continue
		}
		if item.ChannelType == channel.TypeChannel {
			logger.Warn(ctx).Msgf("channel server is not sport: %v", item.ChannelType)
			continue
		}
	}

	err = msg.messageBoxClient.Query().
		Where(messagebox.MemberID(req.MemberId), messagebox.ChannelIDIn(req.ChannelIds...)).
		GroupBy(messagebox.FieldMemberID, messagebox.FieldChannelID).
		Aggregate(ent.Count()).
		Scan(ctx, &resp)

	if err != nil {
		return nil, err
	}
	return resp, nil
}

func (server MessageServer) GetNewMessageByMemberId(ctx context.Context, req *QueryMessageReq) ([]*ent.MessageBox, error) {
	messages, err := server.messageBoxClient.Query().
		Where(messagebox.MemberID(req.MemberId), messagebox.ChannelIDIn(req.ChannelIds...)).All(ctx)
	if err != nil {
		logger.Error(ctx).Err(err).Msg("Failed to query new messages")
		return nil, err
	}
	return messages, nil
}

func (msg *MessageServer) ShowMessageTotal(ctx context.Context, req *QueryMessageReq) (int, error) {
	count, err := msg.messageBoxClient.Query().Where(messagebox.MemberID(req.MemberId)).Count(ctx)
	if err != nil {
		logger.Error(ctx).Err(err).Msg("Failed to query new messages")
		return 0, err
	}
	logger.Debug(ctx).Msgf("new messages count: %v", count)
	return count, nil
}
