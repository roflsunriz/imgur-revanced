package app.revanced.extension.imgur;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

final class MediaLinks {
    private MediaLinks() {
    }

    static String imageUrl(Object image) {
        if (image == null) {
            return null;
        }
        String currentUrl = stringValue(image, "getLink");
        String mimeType = stringValue(image, "getMimeType");
        if (mimeType == null) {
            mimeType = stringValue(image, "getType");
        }
        if (currentUrl == null) {
            currentUrl = stringValue(image, "getUrl");
        }
        return LinkPolicy.imageDirectUrl(stringValue(image, "getId"), mimeType, currentUrl);
    }

    static String firstPostImageUrl(Object post) {
        Object items = value(post, "getPostMediaContentItems");
        if (items == null) {
            Object images = value(post, "getImages");
            if (images instanceof List<?> && !((List<?>) images).isEmpty()) {
                String url = imageUrl(((List<?>) images).get(0));
                return LinkPolicy.isDirectImageUrl(url) ? url : null;
            }
        }
        if (!(items instanceof List<?>) || ((List<?>) items).isEmpty()) {
            return null;
        }
        Object content = ((List<?>) items).get(0);
        for (String getter : new String[]{"getLegacyImage", "getImageData", "getLegacyVideo", "getVideoData"}) {
            Object image = value(content, getter);
            if (image != null) {
                String url = imageUrl(image);
                return LinkPolicy.isDirectImageUrl(url) ? url : null;
            }
        }
        return null;
    }

    static String permalinkImageUrl(Object holder) {
        if (holder == null) {
            return null;
        }
        try {
            Field field = holder.getClass().getDeclaredField("boundItem");
            field.setAccessible(true);
            return firstPostImageUrl(field.get(holder));
        } catch (NoSuchFieldException ignored) {
            try {
                Field field = holder.getClass().getDeclaredField("presenter");
                field.setAccessible(true);
                return firstPostImageUrl(value(field.get(holder), "getPost"));
            } catch (ReflectiveOperationException | SecurityException unsupported) {
                return null;
            }
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    static String stringValue(Object target, String getter) {
        Object result = value(target, getter);
        return result instanceof String ? (String) result : null;
    }

    private static Object value(Object target, String getter) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(getter);
            return method.invoke(target);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            // Unknown future models retain the stock URL rather than crashing the copy action.
            return null;
        }
    }
}
