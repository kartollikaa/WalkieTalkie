package com.kartollika.walkietalkie.bluetooth.di

import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface BluetoothDataSourceModule {

  @Binds
  @Singleton
  fun bindsBlueToothActionsDataSource(bluetoothActionsDataSource: BluetoothActionsDataSourceImpl): BluetoothActionsDataSource
}