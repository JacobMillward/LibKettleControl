package com.jacobmillward.kettlecontrol;

public enum KettleStatus {
		SEL_100C("0x100"),
		SEL_95C("0x95"),
		SEL_80C("0x80"),
		SEL_65C("0x65"),
		SEL_WARM("0x11"),
		SEL_WARM_5("0x8005"),
		SEL_WARM_10("0x8010"),
		SEL_WARM_20("0x8020"),
		WARM_END("0x10"),
		TEMP_REACHED("0x3"),
		KETTLE_REMOVED("0x1"),
		TURNED_ON("0x5"),
		TURNED_OFF("0x0"),
		PROBLEM("0x2"),
		HELLO("HELLOAPP");
		
		private final String code;
		
		KettleStatus(String code) {
			this.code = code;
		}
		
		public String code() {
			return code;
		}
		
	}