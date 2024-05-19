package foundation.oned6.dicegrid.protocol;

// struct node_state {
//    bool shutdown;
//    bool engaged;
//
//    double current_rms_inner;
//    double current_rms_outer;
//    double voltage_rms;
//
//    double current_freq_inner;
//    double current_freq_outer;
//    double voltage_freq;
//
//    double phase_angle;
//    double currents_angle;
//
//    double current_thd_inner;
//    double current_thd_outer;
//    double voltage_thd;
//};
public record NodeState(
	boolean shutdown,
	boolean engaged,

	double currentRmsInner,
	double currentRmsOuter,
	double voltageRms,

	double currentFreqInner,
	double currentFreqOuter,
	double voltageFreq,

	double phaseAngle,
	double currentsAngle,

	double currentThdInner,
	double currentThdOuter,
	double voltageThd
) {
}
