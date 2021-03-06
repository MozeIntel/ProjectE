package moze_intel.projecte.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import javax.annotation.Nonnull;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class PedestalRenderer extends TileEntityRenderer<DMPedestalTile> {

	public PedestalRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(@Nonnull DMPedestalTile te, float partialTick, @Nonnull MatrixStack matrix, @Nonnull IRenderTypeBuffer renderer, int light, int overlayLight) {
		if (!te.isRemoved() && te.getWorld() != null) {
			if (Minecraft.getInstance().getRenderManager().isDebugBoundingBox()) {
				matrix.push();
				BlockPos pos = te.getPos();
				AxisAlignedBB aabb = te.getEffectBounds().offset(-pos.getX(), -pos.getY(), -pos.getZ());
				IVertexBuilder vertexBuilder = renderer.getBuffer(RenderType.getLines());
				WorldRenderer.drawBoundingBox(matrix, vertexBuilder, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX + 1, aabb.maxY + 1, aabb.maxZ + 1,
						1, 0, 1, 1, 1, 0, 1);
				matrix.pop();
			}
			ItemStack stack = te.getInventory().getStackInSlot(0);
			if (!stack.isEmpty()) {
				matrix.push();
				matrix.translate(0.5, 0.7, 0.5);
				long gameTime = te.getWorld().getGameTime();
				matrix.translate(0, MathHelper.sin((gameTime + partialTick) / 10.0F) * 0.1 + 0.1, 0);
				matrix.scale(0.75F, 0.75F, 0.75F);
				float angle = (gameTime + partialTick) / 20.0F * (180F / (float) Math.PI);
				matrix.rotate(Vector3f.YP.rotationDegrees(angle));
				Minecraft.getInstance().getItemRenderer().renderItem(stack, TransformType.GROUND, light, overlayLight, matrix, renderer);
				matrix.pop();
			}
		}
	}
}