/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.util;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import appeng.api.config.Setting;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigManagerListener;
import appeng.api.util.UnsupportedSettingException;

public final class ConfigManager implements IConfigManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private final Map<Setting<?>, Enum<?>> settings = new IdentityHashMap<>();
    @Nullable
    private final IConfigManagerListener listener;

    public ConfigManager(IConfigManagerListener listener) {
        this.listener = listener;
    }

    public ConfigManager(Runnable changeListener) {
        this.listener = (manager, setting) -> changeListener.run();
    }

    @Override
    public Set<Setting<?>> getSettings() {
        return this.settings.keySet();
    }

    public <T extends Enum<T>> void registerSetting(Setting<T> setting, T defaultValue) {
        this.settings.put(setting, defaultValue);
    }

    @Override
    public <T extends Enum<T>> T getSetting(Setting<T> setting) {
        var oldValue = this.settings.get(setting);

        if (oldValue == null) {
            throw new UnsupportedSettingException("Setting " + setting.getName() + " is not supported.");
        }

        return setting.getEnumClass().cast(oldValue);
    }

    @Override
    public <T extends Enum<T>> void putSetting(Setting<T> setting, T newValue) {
        if (!settings.containsKey(setting)) {
            throw new UnsupportedSettingException("Setting " + setting.getName() + " is not supported.");
        }
        this.settings.put(setting, newValue);
        if (this.listener != null) {
            this.listener.onSettingChanged(this, setting);
        }
    }

    /**
     * save all settings using config manager.
     *
     * @param tagCompound to be written to compound
     * @param registries
     */
    @Override
    public void writeToNBT(CompoundTag tagCompound, HolderLookup.Provider registries) {
        for (var entry : this.settings.entrySet()) {
            tagCompound.putString(entry.getKey().getName(), this.settings.get(entry.getKey()).toString());
        }
    }

    /**
     * read all settings using config manager.
     *
     * @param tagCompound to be read from compound
     * @param registries
     */
    @Override
    public boolean readFromNBT(CompoundTag tagCompound, HolderLookup.Provider registries) {
        boolean anythingRead = false;
        for (var setting : this.settings.keySet()) {
            if (tagCompound.contains(setting.getName(), Tag.TAG_STRING)) {
                String value = tagCompound.getString(setting.getName());
                try {
                    setting.setFromString(this, value);
                    anythingRead = true;
                } catch (IllegalArgumentException e) {
                    LOG.warn("Failed to load setting {} from value '{}': {}", setting, value, e.getMessage());
                }
            }
        }
        return anythingRead;
    }

    @Override
    public boolean importSettings(Map<String, String> settings) {
        boolean anythingRead = false;
        for (var setting : this.settings.keySet()) {
            String value = settings.get(setting.getName());
            if (value != null) {
                try {
                    setting.setFromString(this, value);
                    anythingRead = true;
                } catch (IllegalArgumentException e) {
                    LOG.warn("Failed to load setting {} from value '{}': {}", setting, value, e.getMessage());
                }
            }
        }
        return anythingRead;
    }

    @Override
    public Map<String, String> exportSettings() {
        Map<String, String> result = null;
        for (var entry : this.settings.entrySet()) {
            if (result == null) {
                result = new HashMap<>();
            }
            result.put(entry.getKey().getName(), this.settings.get(entry.getKey()).toString());
        }
        return result == null ? Map.of() : Map.copyOf(result);
    }
}
