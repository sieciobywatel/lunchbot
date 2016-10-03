package actors

import actors.LunchbotActor.OutboundMessage
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import commands.CommandParsing
import model.UserId
import slack.SlackUtil
import slack.models.Message
import slack.rtm.SlackRtmConnectionActor.SendMessage
import util.{Formatting, Logging}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by mactur on 29/09/2016.
  */
class LunchbotActor(selfId: String)
  extends Actor
    with Logging
    with Formatting
    with CommandParsing {

  implicit val askTimeout: Timeout = Timeout(1 second)

  val lunchActor: ActorRef = context.actorOf(LunchActor.props)

  override def receive: Receive = {

    case message: Message if SlackUtil.mentionsId(message.text, selfId) =>

      logger.debug(s"Got incoming message: $message")

      val slack = sender()

      val textWithNoMentions = message.text.replaceAll(SlackUtil.mentionrx.toString(), "")

      parse(message.copy(text = textWithNoMentions)) match {

        case Some(command) =>
          (lunchActor ? command)
            .mapTo[OutboundMessage]
            .map(om => SendMessage(message.channel, om.getText))
            .pipeTo(slack)

        case None =>
          slack ! SendMessage(message.channel, s"${formatMention(message.user)} I didn't quite get that...")

      }

  }

}

object LunchbotActor extends Formatting {

  def props(selfId: String): Props = Props(new LunchbotActor(selfId))

  sealed trait OutboundMessage {
    def getText: String
  }

  case class HereMessage(text: String) extends OutboundMessage {
    override def getText: String = s"<!here> $text"
  }

  case class MentionMessage(text: String, mentionedUser: UserId) extends OutboundMessage {
    override def getText: String = s"${formatMention(mentionedUser)} $text"
  }

  case class SimpleMessage(text: String) extends OutboundMessage {
    override def getText: String = text
  }

}