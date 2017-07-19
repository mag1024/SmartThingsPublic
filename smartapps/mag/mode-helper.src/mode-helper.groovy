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
    name: "Mode Helper",
    namespace: "mag",
    author: "Michael G",
    description: "Installation specific mode actions.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Away"){
		input "awayThermos", "capability.thermostat", title: "Thermostats", multiple: true
        input(name: "awayThermoMode", type: "enum", title: "Mode", options: ["heat","cool"])
        input "awaySwitches", "capability.switch", title: "Switches", multiple: true
	}
	section("Night"){
		input "nightThermos", "capability.thermostat", title: "Thermostats", multiple: true
        input "nightOffset", "number", title: "Temperature Offset"
	}
}

def installed() {
    subscribe(location, modeHandler)
    state.appVersion = "1.0-2"
    state.lastMode = "Home"
    state.lastChange = now()
}

def updated() {
	unsubscribe()
    unschedule()
	installed()
}

def modeHandler(evt) {
	def mode = evt.value
	def lastMode = state.lastMode
	log.debug "mode change: $lastMode -> $mode"
    if (mode == lastMode) return
    if (mode == "Away") onAway()
    if (lastMode == "Away") onArrive()
    if (mode == "Night") onSleep()
    if (lastMode == "Night") onWake()
    state.lastMode = mode
    state.lastActivity = now()
}

private onAway() {
	log.debug "onAway()"
	awayThermos.setThermostatMode("off")
}

private onArrive() {
	log.debug "onArrive()"
	awayThermos.setThermostatMode(awayThermoMode)
    def daytime = getSunriseAndSunset()
    if (!timeOfDayIsBetween(daytime.sunrise, daytime.sunset, new Date(),
    						location.timeZone)) {
		awaySwitches.on()
    }
}

private setThermoPoints(t, points, offset) {
	log.debug "Setting $t.name to $points + $offset"
    t.setCoolingSetpoint(points[0] + offset)
    t.setHeatingSetpoint(points[1] + offset)
}

private onSleep() {
	log.debug "onSleep()"
	state.dayTemp = [:]
    for (t in nightThermos) {
        log.debug "onSleep(): thermo = $t"
    	def points = [t.currentCoolingSetpoint,  t.currentHeatingSetpoint]
        log.debug "onSleep(): points = $points"
    	state.dayTemp[t.name] = points
        setThermoPoints(t, points, nightOffset)
    }
}

private onWake() {
	log.debug "onWake()"
    for (t in nightThermos) setThermoPoints(t, state.dayTemp[t.name], 0)
}