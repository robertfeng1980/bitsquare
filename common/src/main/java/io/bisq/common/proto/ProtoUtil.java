/*
 * This file is part of bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.common.proto;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.bisq.common.Payload;
import io.bisq.common.locale.CurrencyUtil;
import io.bisq.generated.protobuffer.PB;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ProtoUtil {

    public static Set<byte[]> getByteSet(List<ByteString> byteStringList) {
        return byteStringList.stream().map(ByteString::toByteArray).collect(Collectors.toSet());
    }

    public static String getCurrencyCode(PB.OfferPayload pbOffer) {
        return CurrencyUtil.isCryptoCurrency(pbOffer.getBaseCurrencyCode()) ? pbOffer.getBaseCurrencyCode() : pbOffer.getCounterCurrencyCode();
    }

    /**
     * Returns the input String, except when it's the empty string: "", then null is returned.
     * Note: "" is the default value for a protobuffer string, so this means it's not filled in.
     */
    public static String emptyStringToNull(String stringFromProto) {
        return "".equals(stringFromProto) ? null : stringFromProto;
    }

    /**
     * Get a Java enum from a Protobuf enum in a safe way.
     *
     * @param enumType the class of the enum, e.g: BlaEnum.class
     * @param name the name of the enum entry, e.g: proto.getWinner().name()
     * @param <E> the enum Type
     * @return an enum
     */
    public static <E extends Enum<E>> E enumFromProto(Class<E> enumType, String name) {
        E result = null;
        try {
            result = Enum.valueOf(enumType, name);
        } catch (IllegalArgumentException err) {
            log.error("Invalid value for enum " + enumType.getSimpleName() + ": " + name, err);
        }

        return result;
    }

    public static <T extends Message> Iterable<T> collectionToProto(Collection<? extends Payload> collection) {
        return collection.stream()
                .map(e -> {
                    final Message message = e.toProtoMessage();
                    try {
                        //noinspection unchecked
                        return (T) message;
                    } catch (Throwable t) {
                        log.error("message could not be casted. message=" + message);
                        return null;
                    }
                })
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }

    public static <T> Iterable<T> collectionToProto(Collection<? extends Payload> collection, Function<? super Message, T> extra) {
        return collection.stream().map(o -> extra.apply(o.toProtoMessage())).collect(Collectors.toList());
    }
}