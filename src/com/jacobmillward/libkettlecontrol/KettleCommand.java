package com.jacobmillward.libkettlecontrol;

public enum KettleCommand {
		BTN_100C("0x80"),
		BTN_95C("0x2"),
		BTN_80C("0x4000"),
		BTN_65C("0x200"),
		BTN_WARM("0x8"),
		BTN_WARM_5("0x8005"),
		BTN_WARM_10("0x8010"),
		BTN_WARM_20("0x8020"),
		BTN_ON("0x4"),
		BTN_OFF("0x0");
		
		private final String code;
		
		KettleCommand(String code) {
			this.code = code;
		}
		
		public String code() {
			return code;
		}
	}