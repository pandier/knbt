package net.benwoodworth.knbt

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import net.benwoodworth.knbt.internal.*

public sealed interface NbtFormat : SerialFormat {
    public val configuration: NbtFormatConfiguration

    /**
     * Serializes and encodes the given [value] to an [NbtTag] using the given [serializer].
     */
    public fun <T> encodeToNbtTag(serializer: SerializationStrategy<T>, value: T): NbtTag {
        lateinit var result: NbtTag
        encodeToNbtWriter(TreeNbtWriter { result = it }, serializer, value)
        return result
    }

    /**
     * Decodes and deserializes the given [tag] to a value of type [T] using the given [deserializer].
     */
    public fun <T> decodeFromNbtTag(deserializer: DeserializationStrategy<T>, tag: NbtTag): T =
        decodeFromNbtReader(TreeNbtReader(tag), deserializer)
}

public sealed interface NbtFormatBuilder {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     */
    public var encodeDefaults: Boolean

    /**
     * Specifies whether encounters of unknown properties in the input NBT
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * "type" by default.
     */
    public var classDiscriminator: String

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [NbtFormat] instance.
     */
    public var serializersModule: SerializersModule
}

/**
 * Serializes and encodes the given [value] to an [NbtTag] using
 * serializer retrieved from the reified type parameter.
 */
public inline fun <reified T> NbtFormat.encodeToNbtTag(value: T): NbtTag =
    encodeToNbtTag(serializersModule.serializer(), value)

/**
 * Decodes and deserializes the given [tag] to a value of type [T] using
 * serializer retrieved from the reified type parameter.
 */
public inline fun <reified T> NbtFormat.decodeFromNbtTag(tag: NbtTag): T =
    decodeFromNbtTag(serializersModule.serializer(), tag)

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal fun <T> NbtFormat.encodeToNbtWriter(writer: NbtWriter, serializer: SerializationStrategy<T>, value: T) {
    val rootSerializer = if (
        serializer.descriptor.kind == StructureKind.CLASS ||
        serializer is AbstractPolymorphicSerializer
    ) {
        RootClassSerializer(serializer)
    } else {
        serializer
    }

    return DefaultNbtEncoder(this, writer)
        .encodeSerializableValue(rootSerializer, value)
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal fun <T> NbtFormat.decodeFromNbtReader(reader: NbtReader, deserializer: DeserializationStrategy<T>): T {
    val rootDeserializer = if (
        deserializer.descriptor.kind == StructureKind.CLASS ||
        deserializer is AbstractPolymorphicSerializer
    ) {
        RootClassDeserializer(deserializer)
    } else {
        deserializer
    }

    return NbtDecoder(this, reader).decodeSerializableValue(rootDeserializer)
}
