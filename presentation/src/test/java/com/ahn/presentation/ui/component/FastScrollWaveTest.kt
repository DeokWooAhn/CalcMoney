package com.ahn.presentation.ui.component

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe

class FastScrollWaveTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val radius = 60f

        Given("손가락 바로 아래 라벨일 때") {
            When("물결 세기를 구하면") {
                Then("가장 센 1이어야 한다") {
                    waveInfluence(distance = 0f, radius = radius) shouldBe 1f
                }
            }
        }

        Given("물결 반경 끝이나 그 바깥의 라벨일 때") {
            When("물결 세기를 구하면") {
                Then("0이어야 한다") {
                    waveInfluence(distance = radius, radius = radius) shouldBe 0f
                    waveInfluence(distance = radius * 2, radius = radius) shouldBe 0f
                }
            }
        }

        Given("반경 안의 라벨일 때") {
            When("손가락에서 멀어지면") {
                Then("세기가 점점 줄어야 한다") {
                    val near = waveInfluence(distance = 10f, radius = radius)
                    val middle = waveInfluence(distance = 30f, radius = radius)
                    val far = waveInfluence(distance = 50f, radius = radius)

                    near shouldBeLessThan 1f
                    near shouldBeGreaterThan middle
                    middle shouldBeGreaterThan far
                    far shouldBeGreaterThan 0f
                }
            }

            When("반경의 절반 거리이면") {
                Then("세기가 절반이어야 한다") {
                    waveInfluence(distance = radius / 2, radius = radius) shouldBe (0.5f plusOrMinus 0.0001f)
                }
            }

            When("손가락 위쪽과 아래쪽으로 같은 거리이면") {
                Then("세기가 같아야 한다") {
                    val above = waveInfluence(distance = -20f, radius = radius)
                    val below = waveInfluence(distance = 20f, radius = radius)

                    above shouldBe below
                }
            }
        }

        Given("반경이 0 이하일 때") {
            When("물결 세기를 구하면") {
                Then("물결이 없어야 한다") {
                    waveInfluence(distance = 0f, radius = 0f) shouldBe 0f
                    waveInfluence(distance = 0f, radius = -1f) shouldBe 0f
                    waveRipple(distance = 0f, radius = 0f) shouldBe 0f
                }
            }
        }

        Given("위아래 라벨이 밀려나고 흐려지는 정도를 구할 때") {
            When("물결 중심의 라벨이면") {
                Then("선택된 라벨은 밀리거나 흐려지지 않아야 한다") {
                    waveRipple(distance = 0f, radius = radius) shouldBe 0f
                }
            }

            When("반경의 절반 거리이면") {
                Then("가장 크게 반응해야 한다") {
                    waveRipple(distance = radius / 2, radius = radius) shouldBe (1f plusOrMinus 0.0001f)
                }
            }

            When("반경 끝이나 그 바깥이면") {
                Then("반응하지 않아야 한다") {
                    waveRipple(distance = radius, radius = radius) shouldBe (0f plusOrMinus 0.0001f)
                    waveRipple(distance = radius * 2, radius = radius) shouldBe 0f
                }
            }

            When("위쪽과 아래쪽으로 같은 거리이면") {
                Then("반응 크기가 같아야 한다") {
                    val above = waveRipple(distance = -20f, radius = radius)
                    val below = waveRipple(distance = 20f, radius = radius)

                    above shouldBe below
                }
            }
        }
    })
