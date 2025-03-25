package channel

import (
	"context"
	"entgo.io/ent/dialect/sql"
	"errors"
	"im/db"
	"im/ent"
	"im/ent/channel"
	"im/ent/channelbox"
	conversations2 "im/ent/conversations"
	"im/pkg/logger"
)

type ChannelServer struct {
	channelClient    ent.ChannelClient
	channelBoxClient ent.ChannelBoxClient
	db               db.Context
}

func NewChannelServer(db db.Context) *ChannelServer {
	return &ChannelServer{db: db,
		channelClient:    *db.GetClient().Channel,
		channelBoxClient: *db.GetClient().ChannelBox,
	}
}

type CreateChannelReq struct {
	MemberFromId int          `json:"memberFromId"`
	MemberToId   int          `json:"memberToId"`
	ChannelType  channel.Type `json:"channelType"`
}

type QueryChannelReq struct {
	MemberId    int
	ChannelType channel.Type
}

func (server *ChannelServer) CreatChannel(ctx context.Context, req *CreateChannelReq) error {
	return server.db.WithTx(ctx, func(tx *ent.Tx) error {
		_channel, err := tx.Channel.Create().SetType(req.ChannelType).Save(ctx)
		if err != nil {
			logger.Fatal(ctx).Err(err).Msgf("Failed to create channel,req:%v", req)
			return err
		}
		logger.Debug(ctx).Msgf("Channel created: %v", _channel)
		//TODO: move to channel domain
		switch req.ChannelType {
		case channel.TypeConversation:
			{
				conversations, err := tx.Conversations.Create().
					SetChannelID(_channel.ID).
					SetMemberFromID(req.MemberFromId).
					SetMemberToID(req.MemberToId).
					SetType(conversations2.TypeTemp).
					Save(ctx)

				if err != nil {
					logger.Fatal(ctx).Err(err).Msgf("Failed to create conversations,req:%v", req)
					return err
				}
				logger.Debug(ctx).Msgf("Conversation created: %v", conversations)
			}
		case channel.TypeChannel:
			{
				return errors.New("channel type not supported")
			}
		default:
			{
				return errors.New("channel type not supported")
			}

		}

		if req.ChannelType == channel.TypeConversation {

			conversations, err := tx.Conversations.Create().
				SetChannelID(_channel.ID).
				SetMemberFromID(req.MemberFromId).
				SetMemberToID(req.MemberToId).
				SetType(conversations2.TypeTemp).
				Save(ctx)

			if err != nil {
				logger.Fatal(ctx).Err(err).Msgf("Failed to create conversations,req:%v", req)
				return err
			}
			logger.Debug(ctx).Msgf("Conversation created: %v", conversations)
		}

		box, err := tx.ChannelBox.Create().SetChannelID(_channel.ID).SetMemberID(req.MemberFromId).Save(ctx)
		if err != nil {
			logger.Fatal(ctx).Err(err).Msgf("Failed to create channel box,req:%v", req)
			return err
		}
		logger.Debug(ctx).Msgf("ChannelBox created: %v", box)
		return nil
	})
}

type ChannelRes struct {
	Id          int
	MemberId    int
	ChannelId   int
	ChannelType channel.Type
}

func (server ChannelServer) GetChannelItems(ctx context.Context, req *QueryChannelReq) ([]ChannelRes, error) {
	// build query
	boxQuery := server.channelBoxClient.Query()
	if req.ChannelType == channel.TypeConversation {
		boxQuery = boxQuery.WithChannels(func(query *ent.ChannelQuery) {
			query.Where(channel.TypeEQ(req.ChannelType))
		})
		return nil, errors.New(string(req.ChannelType + " type not supported"))
	}
	var res []ChannelRes
	err := boxQuery.Where(channelbox.MemberIDEQ(req.MemberId)).Select(channelbox.FieldChannelID).Aggregate(func(selector *sql.Selector) string {
		t := sql.Table(channel.Table)
		selector.Join(t).On(channelbox.FieldChannelID, t.C(channel.FieldID))
		return sql.As(t.C(channel.FieldType), "channelType")
	}).Scan(ctx, &res)

	if err != nil {
		logger.Error(ctx).Err(err).Msgf("Failed to query channel items,req:%v", req)
		return nil, err
	}
	logger.Debug(ctx).Msgf("Channel items found: %v", req)
	return res, nil
}
