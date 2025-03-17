#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:3.2.0")

@file:Repository("https://bindings.krzeminski.it/")
@file:DependsOn("actions:checkout___major:[v4,v5-alpha)")

import io.github.typesafegithub.workflows.actions.actions.Checkout
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.triggers.PullRequest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.workflow

workflow(
    name = "Check all Workflow YAML Consistency",
    on = listOf(
        Push(),
        PullRequest()
    ),
    sourceFile = __FILE__
) {
    job(
        id = "check_all_workflow_yaml_consistency",
        name = "Check all Workflow YAML Consistency",
        runsOn = UbuntuLatest
    ) {
        uses(
            name = "Checkout",
            action = Checkout()
        )

        run(
            name = "Regenerate all Workflow YAMLs",
            command = """find .github/workflows -mindepth 1 -maxdepth 1 -name '*.main.kts' -exec {} \;"""
        )

        run(
            name = "Check for Modifications",
            command = """
                git add --intent-to-add .
                git diff --exit-code
            """.trimIndent()
        )
    }
}
