# World Bundler

An experimental, alpha-quality world compressor and region recovery software specialised for Minecraft.

> [!CAUTION]
> World Bundler is not ready for production use.
> The bundler format is not stable,
> and there is currently no guarantee that it can be read if you compress with it now.

## Bundling

TODO: This part of the project is not finished.

## Recovery

*Anvil and McRegion world recovery*

> [!TIP]
> Take backups for everything you find valuable.
>
> This is a tool that you hope you never need but glad you have.
> However, it can't always fix your mistakes,
> particularly if it has been anywhere from several minutes to hours since deletion.
>
> If you're looking at this tool as a backup measure,
> first consider taking proper full-copy backups of your worlds.

### How to use

1. Use a secondary file recovery tool
	- It is important that you do this as soon as possible.
	  The longer you wait, the more likely your worlds will be irrecoverably overwritten by competing processes.
2. Prepare the source folder.
3. Prepare a target folder.
	- You can use an older backup if there is one available.
	  Bundler can directly write into an existing world and stack newer chunks over the older region.
	- Note: The backup must not have been opened within Minecraft since creation.
4. Run the following command: `java -cp world-bundler.jar gay.ampflower.bundler.recovery.Recovery input/ output/`
	- This may take a few minutes to a few hours, depending on how many region candidates you have, and the size of each
	  candidate.

### How does it work?

Bundler first attempts to read each region it encounters normally, to let it parse with a fast path.
It then rereads the region aligned to 512-byte sectors...

1. Treating each sector as if it was the start of a chunk.
2. Sniffing the compressor used by magic file, then trying it at offset of 5 and 0.

The reread bypasses damage in the first sector, allowing for chunks that were otherwise unindexed to be detected and
read.
This can occasionally bring an older revision of a chunk back to life if a newer revision can't be found,
as McRegion can relocate chunks to anywhere else within the region as needed,
and starting with Minecraft 1.15, Anvil can store chunks externally in `c.x.z.mcc` files.
