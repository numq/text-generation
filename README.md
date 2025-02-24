# Text generation

JVM library for text generation, written in Kotlin and based on the C++
library [llama.cpp](https://github.com/ggml-org/llama.cpp)

## Features

- Generate text from a string

## Installation

- Download latest [release](https://github.com/numq/text-generation/releases)

- Add library dependency
   ```kotlin
   dependencies {
        implementation(file("/path/to/jar"))
   }
   ```

- Unzip binaries

## Usage

### TL;DR

> See the [example](example) module for implementation details

- Call `generate` to process the string and get a generated output

### Step-by-step

- Load binaries
    - CPU
       ```kotlin
       TextGeneration.Llama.loadCPU(
        ggmlBase = "/path/to/ggml-base", 
        ggmlCpu = "/path/to/ggml-cpu",
        ggmlRpc = "/path/to/ggml-rpc",
        ggml = "/path/to/ggml",
        llama = "/path/to/llama",
        textGeneration = "/path/to/text-generation",
      )
       ```
    - CUDA
       ```kotlin
       TextGeneration.Llama.loadCPU(
        ggmlBase = "/path/to/ggml-base", 
        ggmlCpu = "/path/to/ggml-cpu",
        ggmlCuda = "/path/to/ggml-cuda",
        ggmlRpc = "/path/to/ggml-rpc",
        ggml = "/path/to/ggml",
        llama = "/path/to/llama",
        textGeneration = "/path/to/text-generation",
      )
       ```

- Create an instance

  ```kotlin
  TextGeneration.Llama.create(
      modelPath = "/path/to/model"
  )
  ```

- Call `history` to get the history of text generation


- Call `generate` to process the string and get a generated output


- Call `reset` to reset the internal state and history


- Call `close` to release resources

## Requirements

- JVM version 9 or higher

## License

This project is licensed under the [Apache License 2.0](LICENSE)

## Acknowledgments

- [llama.cpp](https://github.com/ggml-org/llama.cpp)
