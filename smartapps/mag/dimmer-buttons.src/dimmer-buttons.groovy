definition(
    name: "Dimmer Buttons",
    namespace: "mag",
    author: "Michael G",
    description: "2 Button switch -> Dimmer",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    input "buttons", "capability.button", title: "Button", required: true
    input "dimmer", "capability.switchLevel", title: "Dimmer", required: true
}

def installed() {
    subscribe(buttons, "button", buttonHandler)
    state.appVersion = "0.1"
}

def updated() {
	unsubscribe()
	installed()
}

def buttonHandler(evt) {
	def action = evt.value
    def buttonNumber = new groovy.json.JsonSlurper().parseText(evt.data).buttonNumber.toInteger()
    log.debug "buttonEvent: $evt.name = $action ($buttonNumber)"
  	if (["pushed", "held"].contains(action) == false) return
 	
    if (action == "pushed" && buttonNumber == 2) {
    	dimmer.off()
    } else {
        dimmer.on()
        def level = 100
        if (action == "held") level = (buttonNumber == 1 ? 50 : 15)
        dimmer.setLevel(level, 2)
    }
}