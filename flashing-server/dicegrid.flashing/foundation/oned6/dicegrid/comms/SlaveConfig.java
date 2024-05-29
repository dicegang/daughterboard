package foundation.oned6.dicegrid.comms;

import com.pi4j.io.spi.SpiChipSelect;

public class SlaveConfig {
	static final int CLOCK_SPEED = 20_000_000;
	static final SpiChipSelect SS = SpiChipSelect.CS_0;
	static final int SLAVE_BUS = 0;

	static final int MISO_HANDSHAKE_GPIO = 20;
	static final int MOSI_HANDSHAKE_GPIO = 19;
}
