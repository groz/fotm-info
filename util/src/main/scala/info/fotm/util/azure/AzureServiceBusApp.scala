package info.fotm.util.azure

import com.microsoft.windowsazure.services.servicebus._
import com.microsoft.windowsazure.services.servicebus.models._

import scala.util.Try

object AzureServiceBusApp extends App {
  val config =
    com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration.configureWithSASAuthentication(
      AzureSettings.serviceBusNamespace,  // namespace
      "RootManageSharedAccessKey",        // sasKeyName
      AzureSettings.serviceBusKey,        // sasKey
      ".servicebus.windows.net"           // serviceBusRootUri
    )

  val topicName = "team-updates"

  val serviceBus = ServiceBusService.create(config)

  val subName = "AllMessages"
  val subInfo: SubscriptionInfo =
    Try(serviceBus.createSubscription(topicName, new SubscriptionInfo(subName)).getValue)
    .getOrElse(serviceBus.getSubscription(topicName, subName).getValue)

  val opts = ReceiveMessageOptions.DEFAULT.setReceiveMode(ReceiveMode.PEEK_LOCK)

  while (true) {
    println(s"Count: ${subInfo.getCountDetails.getActiveMessageCount}")

    val resultSubMsg = serviceBus.receiveSubscriptionMessage(topicName, subName, opts)
    val messageOption = Option(resultSubMsg.getValue)

    for (message <- messageOption) {
      println("MessageID: " + message.getMessageId)
      val res = scala.io.Source.fromInputStream(message.getBody, "UTF-8")
      println(res.mkString)
      //serviceBus.deleteMessage(message)
    }
    readLine()
  }
}
