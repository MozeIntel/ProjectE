package moze_intel.projecte.network.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import moze_intel.projecte.emc.EMCMapper;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.emc.SimpleStack;
import moze_intel.projecte.playerData.Transmutation;
import moze_intel.projecte.utils.PELogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ClientSyncEmcPKT implements IMessage, IMessageHandler<ClientSyncEmcPKT, IMessage>
{
	private int packetNum;
	private Object[] data;

	public ClientSyncEmcPKT() {}

	public ClientSyncEmcPKT(int packetNum, ArrayList<Double[]> arrayList)
	{
		this.packetNum = packetNum;
		data = arrayList.toArray();
	}

	@Override
	public IMessage onMessage(ClientSyncEmcPKT pkt, MessageContext ctx)
	{
		if (pkt.packetNum == 0)
		{
			PELogger.logInfo("Receiving EMC data from server.");

			EMCMapper.emc.clear();
			EMCMapper.emc = new LinkedHashMap<SimpleStack, Double>();
		}

		for (Object obj : pkt.data)
		{
			Double[] array = (Double[]) obj;

			SimpleStack stack = new SimpleStack((int)Math.floor(array[0]), (int)Math.floor(array[1]), (int)Math.floor(array[2]));

			if (stack.isValid())
			{
				EMCMapper.emc.put(stack, array[3]);
			}
		}

		if (pkt.packetNum == -1)
		{
			PELogger.logInfo("Received all packets!");

			Transmutation.loadCompleteKnowledge();
			FuelMapper.loadMap();
		}

		return null;
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		packetNum = buf.readInt();
		int size = buf.readInt();
		data = new Object[size];

		for (int i = 0; i < size; i++)
		{
			Double[] array = new Double[4];

			for (int j = 0; j < 4; j++)
			{
				array[j] = buf.readDouble();
			}

			data[i] = array;
		}
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(packetNum);
		buf.writeInt(data.length);

		for (Object obj : data)
		{
			Double[] array = (Double[]) obj;

			for (int i = 0; i < 4; i++)
			{
				buf.writeDouble(array[i]);
			}
		}
	}
}
