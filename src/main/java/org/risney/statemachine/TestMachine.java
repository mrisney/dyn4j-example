package org.risney.statemachine;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.delegates.Action;
import com.google.gson.Gson;

public class TestMachine {

	private Logger log = LoggerFactory.getLogger(TestMachine.class);

	private StateMachineConfig<State, Trigger> phoneCallConfig;

	private enum State {
		Ringing, Connected, OnHold, OffHook
	}

	private enum Trigger {
		CallDialed, CallConnected, PlacedOnHold, LeftMessage, HungUp
	}

	public void configure() {

		Action callStartTimer = new Action() {
			public void doIt() {
				startCallTimer();
			}
		};
		Action callStopTimer = new Action() {
			public void doIt() {
				stopCallTimer();
			}
		};

		this.phoneCallConfig = new StateMachineConfig<State, Trigger>();
		this.phoneCallConfig.configure(State.OffHook).permit(Trigger.CallDialed, State.Ringing);
		this.phoneCallConfig.configure(State.Ringing).permit(Trigger.HungUp, State.OffHook).permit(Trigger.CallConnected,State.Connected);

		this.phoneCallConfig.configure(State.Connected).onEntry(callStartTimer).onExit(callStopTimer)
				.permit(Trigger.LeftMessage, State.OffHook).permit(Trigger.HungUp, State.OffHook)
				.permit(Trigger.PlacedOnHold, State.OnHold);

		this.phoneCallConfig.configure(State.OnHold).permit(Trigger.LeftMessage, State.OffHook).permit(Trigger.HungUp,
				State.OffHook);
	}

	public void run() {

		Gson gson = new Gson();
		String json = "";
		StateMachine<State, Trigger> phoneCall = new StateMachine<State, Trigger>(State.OffHook, phoneCallConfig);

		phoneCall.fire(Trigger.CallDialed);
		log.debug(phoneCall.getState().toString());
		phoneCall.fire(Trigger.CallConnected);
		json = gson.toJson(phoneCall.getPermittedTriggers());
		log.debug(json);
		phoneCall.fire(Trigger.PlacedOnHold);
		log.debug(phoneCall.getState().toString());
		phoneCall.fire(Trigger.LeftMessage);
		log.debug(phoneCall.getState().toString());
		json = gson.toJson(phoneCall.getPermittedTriggers());
		log.debug(json);

	}

	public void startCallTimer() {
		String timeStamp = new SimpleDateFormat("ss S").format(new Date());
		log.info("starting call timer : " + timeStamp);

	}

	public void stopCallTimer() {
		String timeStamp = new SimpleDateFormat("ss S").format(new Date());
		log.info("stopping call timer : " + timeStamp);
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		TestMachine testMachine = new TestMachine();
		testMachine.configure();
		testMachine.run();
	}
}
