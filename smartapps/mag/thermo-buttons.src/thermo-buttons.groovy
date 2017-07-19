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
    state.appVersion = "0.3"
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
    
    if (location.mode == "Away") {
    	location.setMode("Home") 
    	return
	}
    
    if (buttonNumber <= 2) handleThermoEvent(thermos[buttonNumber % 2])
}

def handleThermoEvent(thermo) {
    def new_temp = thermo.currentTemperature
    def thermoMode = thermo.currentThermostatMode
    def thermoState = thermo.currentThermostatOperatingState
    if (thermoMode == "cool" && thermoState != "cooling") new_temp -= 1
    if (thermoMode == "heat" && thermoState != "heating") new_temp += 1
    
    log.debug "buttonEvent: applying $new_temp to $thermo.name"
    thermo.setCoolingSetpoint(new_temp)
    thermo.setHeatingSetpoint(new_temp)
}
