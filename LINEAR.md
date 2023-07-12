# Linear Documentation as reverse-engineered.

```c
// I can assure you that despite looking like C
// that this will definitely not compile.

// Stuff like magic *struct will definitely not be legal C

typedef byte char;

typedef Q long;
typedef B unsigned byte;
typedef b signed byte;
typedef h signed short;
typedef I unsigned int;

final long regionDimension = 32;
final byte compressionTypeZLib = 0x02;
final byte externalFileCompressionType = 0b10000000 | compressionTypeZLib;
final long linearSignature = 0xc3ff13183cca9d9a; // long;
final List<int> supportedVersion = [1, 2];
final byte linearVersion = 1;

headerSize = regionDimension ** 2 * 8

// 32 bytes
struct header {
	// bigEndian

	Q signature;
	B version;
	Q newestTimestamp;
	b compressionLevel;
	h chunkCount;
	I completeRegionLength;
	Q reserved;
}

// 8 bytes
struct footer {
	// bigEndian
	Q signature;
}

struct linear {
	struct header header;
	byte* zstdContent;
	struct footer footer;
}

struct regionHeader {
	// bigEndian
	I size;
	I timestamp;
}

int main(char* path) {
	stream = open(path);

	if(precondition()) {
		return 1;
	}

	stream.seek(32);

	byte* region = zstd_decompress(stream.readtil(TAIL, -8));

	int sizes[regionDimension ** 2];
	int timestamps[regionDimensions ** 2];
	int count = 0, real = 0, totalSize = 0;

	struct regionHeader header;

	for(; count < regionDimension ** 2; count++) {
		read(*header, stream);
		totalSize += header.size;
		if(header.size != 0) real++;

		sizes[count] = header.size;
		timestamps[count] = header.timestamp;
	}

	// Total size check here
	// if(totalSize + HEADER_SIZE != sizeof(region)) {
	// 	printf("Invalid size %d: Expected %d\n", totalSize + HEADER_SIZE, sizeof(region));
	//	return 1;
	// }

	if (real != chunk) {
		printf("Invalid chunk count %d: Expected %d\n", real, chunk);

	}

}

// Checks signatures
int precondition(STREAM stream) {
	stream.seek(HEAD);
	struct header header {};

	read(*header, stream);

	// Check header signature & version
	if(header.signature != linearSignature) {
		printf("Invalid signature: %d\n", header.signature);
		return 1;
	}

	if(header.version !in supportedVersions) {
		printf("Invalid version: %d\n", header.version);
		return 1;
	}

	stream.seek(TAIL, -8);

	struct footer footer {};
	read(*footer, stream);

	// Frankly not sure why there's a footer but I guess it helps with corruption detection.
	if(footer.signature != linearSignature) {
		printf("Invalid footer: %d\n", footer.signature);
		return 1;
	}

	return 0;
}

```
