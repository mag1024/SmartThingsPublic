/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Forked from (and heavily modifed) Virtual Thermostat by SmartThings
 * 	Author: Michael G
 *
 */
 
definition(
    name: "Virtual Thermostat Driver",
    namespace: "mag",
    author: "SmartThings",
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
	section("When there's been movement from (optional, leave blank to not require motion)..."){
		input "motion", "capability.motionSensor", title: "Motion", required: false
	}
	section("Within this number of minutes..."){
		input "minutes", "number", title: "Minutes", required: false
	}
}

def installed()
{
	subscribe(sensor, "temperature", temperatureHandler)
	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
    setSwitchState(false)
}

def updated()
{
	unsubscribe()
	installed()
}

def temperatureHandler(evt)
{
	thermo.setTemperature(evt.doubleValue)
	evaluateWithMotion(evt.doubleValue)
}

def motionHandler(evt)
{
    def lastTemp = sensor.currentTemperature
    if (lastTemp == null) return
    
	if (evt.value == "active") {
		evaluate(lastTemp)
	} else if (evt.value == "inactive") {
    	evaluateWithMotion(lastTemp)
	}
}

private getSetpoint() {
	def mode = thermo.currentThermostatMode 
	if (mode == "cool") return thermo.currentCoolingSetpoint
	if (mode == "heat") return thermo.currentHeatingSetpoint
    return thermo.currentTemperature
}

private evaluateWithMotion(currentTemp)
{
	if (!hasBeenRecentMotion()) {
        setSwitchState(false)
        return;
    }
    
	evaluate(currentTemp)
}

private evaluate(currentTemp)
{
	def setpoint = getSetpoint()
	log.debug "EVALUATE($currentTemp, $setpoint)"
    def delta = 0
	if (thermo.currentThermostatMode == "cool") {
    	delta = currentTemp - setpoint
	} else if (thermo.currentThermostatMode == "heat") {
    	delta = setpoint - currentTemp
	} else {
    	return
    }
    setSwitchState(delta > 0)
}

private setSwitchState(onoff)
{
    if (onoff) {
        outlets.on()
    } else {
        outlets.off()
    }
    state.lastState = onoff
}

private hasBeenRecentMotion()
{
	if (!motion || !minutes) return true
    
    def deltaMinutes = minutes as Long
    def motionEvents = motion.eventsSince(new Date(now() - (60000 * deltaMinutes)))
    log.trace "Found ${motionEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
    return motionEvents.find { it.value == "active" }
}