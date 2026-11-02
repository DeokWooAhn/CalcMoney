package com.ahn.data.di

import com.ahn.data.crash.FirebaseCrashBreadcrumbs
import com.ahn.domain.crash.CrashBreadcrumbs
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CrashModule {
    @Binds
    abstract fun bindCrashBreadcrumbs(
        impl: FirebaseCrashBreadcrumbs,
    ): CrashBreadcrumbs
}
