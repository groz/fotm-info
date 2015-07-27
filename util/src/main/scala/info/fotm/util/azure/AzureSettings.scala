package info.fotm.util.azure

object AzureSettings {
  val serviceBusNamespace = "fotm-storage"
  val serviceBusKey = ""

  val storageAccount = "fotmstorage"
  val storageAccountKey = ""
  val storageConnectionString = s"DefaultEndpointsProtocol=https;AccountName=$storageAccount;AccountKey=$storageAccountKey"
}
