/**
 *  Xively Logger
 *
 *  Copyright 2015 Michael G
 *
 */
definition(
    name: "Xively Logger",
    namespace: "mag",
    author: "Michael G",
    description: "Xively Logger",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Configure") {
        input "xi_apikey", "text", title: "Xively API Key"
        input "xi_feedid", "number", title: "Xively Feed Id"
        input "devices", "capability.temperatureMeasurement", title: "Which devices", multiple: true
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(devices, "temperature", handleTempEvent)
    devices.each { dev ->
        if (dev.hasCapability("Motion Sensor")) {
            log.debug "Device ${dev.label} has capability Motion Sensor."
            subscribe(dev, "motion", handleMotionEvent)
        }
	}
}

def parseHttpResponse(response) {
    log.debug "response contentType: ${response.contentType}"
    log.debug "response data: ${response.data}"
}
    
def writeChannelData(feed_id, channel, value) {
    log.debug "Logging to Xively: $channel = $value"

    def uri = "https://api.xively.com/v2/feeds/${feed_id}.json"
    def json = """{"version":"1.0.0",
                   "datastreams":[{
                   		"id":"${channel}",
                        "current_value":"${value}"
                  }]}""".stripIndent()

    def headers = [
        "X-ApiKey" : "${xi_apikey}"
    ]

    def params = [
        uri: uri,
        headers: headers,
        body: json
    ]

    httpPutJson(params) {response -> parseHttpResponse(response)}
}

def tagForDevice(device) {
  def r = device.label.tokenize('[]')
  if (r.size() > 1) return r.get(1)
  r = device.label.tokenize(' ')
  return r.get(0).toLowerCase()
}

def handleTempEvent(evt) {
    log.debug "Tempreature event: $evt.doubleValue"
    def prefix = tagForDevice(evt.device)
    writeChannelData(xi_feedid, "${prefix}:temperature", 
                     evt.doubleValue)
}

def handleMotionEvent(evt) {
    log.debug "Motion event: $evt.value"
    def prefix = tagForDevice(evt.device)
    writeChannelData(xi_feedid, "${prefix}:motion",
    				 evt.value == "active" ? 100 : 0)
}