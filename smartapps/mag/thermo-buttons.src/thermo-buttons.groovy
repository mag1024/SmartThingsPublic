definition(
    name: "Thermo Buttons",
    namespace: "mag",
    author: "Michael G",
    description: "Minimote -> 2x Thermostats",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    input "buttons", "capability.button", title: "Button", multiple: true, required: true
    input "thermos", "capability.thermostat", title: "Thermostats", multiple: true, required: true
}

def installed() {
    subscribe(buttons, "button", buttonHandler)
    state.appVersion = "0.2"
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
    
    def offset = (buttonNumber > 2 ? -1 : 1) * (action == "pushed" ? 1 : 3)
    def thermo = thermos[buttonNumber % 2]
    
    log.debug "buttonEvent: applying $offset to $thermo.name"
    thermo.setCoolingSetpoint(thermo.currentCoolingSetpoint + offset)
    thermo.setHeatingSetpoint(thermo.currentHeatingSetpoint + offset)
}

