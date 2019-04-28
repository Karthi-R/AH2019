package com.karthi.awsamplify

data class Reported(val value: Object)
data class State(val reported: Reported)
data class Payload(val state: State)
