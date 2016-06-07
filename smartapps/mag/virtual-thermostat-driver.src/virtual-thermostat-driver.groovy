/**
 *  Copyright 2016 Michael G
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
 
definition(
    name: "Virtual Thermostat Driver",
    namespace: "mag",
    author: "Michael G",
    description: """Control a space heater or window air conditioner in conjunction with any temperature sensor, like a SmartSense Multi.
    				This version takes its input (mode, setpoint, etc) from a Simulated Thermostat instance.
                    This way it can be updated convinently via the app, or other automatition, like Amazon Echo / Alexa.""".stripIndent(),
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Choose a thermostat... "){
		input "thermo", "capability.thermostat", title: "Thermostat"
	}
	section("Select the heater or air conditioner outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("When there's been movement from (optional)..."){
		input "motion", "capability.motionSensor", title: "Motion", required: false
	}
	section("Within this number of minutes..."){
		input "minutes", "number", title: "Minutes", required: false
	}
}

def installed() {
	subscribe(sensor, "temperature", temperatureHandler)
	subscribe(thermo, "thermostat", thermoHandler)
    subscribe(location, modeHandler)
	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
    state.appVersion = "1.1-1"
    state.lastActivity = now()
}

def updated() {
	unsubscribe()
	installed()
}

def temperatureHandler(evt) {
	thermo.setTemperature(evt.doubleValue)
	evaluate("temp")
}

def thermoHandler(evt) {
	evaluate("thermo:" + evt.value)
}

def motionHandler(evt) {
	if (evt.value == "active") state.lastActivity = now()
    evaluate("motion")
}

def modeHandler(evt) {
    if (evt.value == "Home") state.lastActivity = now()
    evaluate("mode")
}

private evaluate(trace) {
	def thermoState = thermo.currentThermostatOperatingState
	def thermoMode = thermo.currentThermostatMode
    def activity = hadRecentActivity()
	log.debug "$trace: mode = $thermoMode, state = $thermoState, activity = $activity"
    if (thermoMode != "off" && thermoState in ["cooling", "heating"] && activity) {
    	outlets.on()
    } else {
    	outlets.off()
    }
}

private hadRecentActivity() {
	if (!motion || !minutes) return true
    def horizon = now() - timeOffset(minutes) as Long
    return state.lastActivity > horizon
}