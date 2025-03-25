package main

//TIP <p>To run your code, right-click the code and select <b>Run</b>.</p> <p>Alternatively, click
// the <icon src="AllIcons.Actions.Execute"/> icon in the gutter and select the <b>Run</b> menu item from here.</p>
import (
	"context"
	"encoding/json"
	"github.com/cloudwego/hertz/pkg/app"
	"github.com/cloudwego/hertz/pkg/app/server"
	"github.com/cloudwego/hertz/pkg/common/utils"
	"github.com/cloudwego/hertz/pkg/protocol/consts"
	"im/db"
	"im/internal/service/message"
	"im/pkg/logger"
)

func main() {
	h := server.Default()
	messageServer := message.NewMessageServer(db.Ctx())

	h.POST("/sendMessage", func(ctx context.Context, c *app.RequestContext) {
		body := c.Request.Body()
		var p message.SendMessageReq
		err := json.Unmarshal(body, &p)
		if err != nil {
			logger.Error(ctx).Err(err).Msg("Error unmarshalling body")
			return
		}
		err = messageServer.SendMessage(ctx, &p)
		if err != nil {
			logger.Error(ctx).Msg("Error sending message")
			return
		}
		c.JSON(consts.StatusOK, utils.H{"message": "success"})
	})
	h.Spin()
}
