# Exemplos de JSON para Testar no Postman

Este documento contém exemplos de JSON para testar a API de Devolução PIX usando o Postman.

## Endpoint

```
POST http://localhost:8080/api/v1/pix/devolucoes/solicitar
```

## Headers

```
Content-Type: application/json
```

## Exemplos de Solicitações Válidas

### 1. Solicitação de Devolução por Fraude Comprovada

```json
{
  "idTransacaoOriginal": "TXID_VALIDA_001",
  "motivo": "FRAUDE_COMPROVADA",
  "cpfClienteSolicitante": "11122233344"
}
```

### 2. Solicitação de Devolução por Falha Operacional do Banco (Aprovação Automática)

```json
{
  "idTransacaoOriginal": "TXID_VALIDA_001",
  "motivo": "FALHA_OPERACIONAL_BANCO",
  "cpfClienteSolicitante": "11122233344"
}
```

### 3. Solicitação de Devolução por Cobrança Indevida

```json
{
  "idTransacaoOriginal": "TXID_VALIDA_001",
  "motivo": "COBRANCA_INDEVIDA",
  "cpfClienteSolicitante": "11122233344"
}
```

### 4. Solicitação que Resultará em Análise Manual

```json
{
  "idTransacaoOriginal": "TXID_PARA_ANALISE_MANUAL_001",
  "motivo": "FRAUDE_COMPROVADA",
  "cpfClienteSolicitante": "77788899900"
}
```

### 5. Solicitação que Resultará em Falha no Processamento Financeiro (Recebedor sem Saldo)

```json
{
  "idTransacaoOriginal": "TXID_RECEBEDOR_SEM_SALDO_006",
  "motivo": "FRAUDE_COMPROVADA",
  "cpfClienteSolicitante": "66677788899"
}
```

## Exemplos de Solicitações Inválidas

### 1. Transação Inexistente

```json
{
  "idTransacaoOriginal": "TXID_INEXISTENTE",
  "motivo": "FRAUDE_COMPROVADA",
  "cpfClienteSolicitante": "11122233344"
}
```

### 2. Solicitante Não é o Pagador Original

```json
{
  "idTransacaoOriginal": "TXID_VALIDA_001",
  "motivo": "FRAUDE_COMPROVADA",
  "cpfClienteSolicitante": "99999999999"
}
```

### 3. Transação Fora do Prazo

```json
{
  "idTransacaoOriginal": "TXID_VALIDA_002",
  "motivo": "FRAUDE_COMPROVADA",
  "cpfClienteSolicitante": "22233344455"
}
```

### 4. Motivo Inválido

```json
{
  "idTransacaoOriginal": "TXID_VALIDA_001",
  "motivo": "MOTIVO_INVALIDO",
  "cpfClienteSolicitante": "11122233344"
}
```

### 5. Campos Obrigatórios Ausentes

```json
{
  "idTransacaoOriginal": "",
  "motivo": "FRAUDE_COMPROVADA",
  "cpfClienteSolicitante": "11122233344"
}
```

## Notas Importantes

1. O sistema possui um repositório mock com as seguintes transações:
   - `TXID_VALIDA_001`: Transação válida, valor R$100.00, pagador CPF 11122233344
   - `TXID_VALIDA_002`: Transação válida mas antiga (90 dias), valor R$50.50, pagador CPF 22233344455
   - `TXID_INVALIDA_PAGADOR`: Transação com pagador diferente, valor R$75.00, pagador CPF 99988877766
   - `TXID_PARA_ANALISE_MANUAL_001`: Transação para análise manual, valor R$250.75, pagador CPF 77788899900
   - `TXID_RECEBEDOR_SEM_SALDO_006`: Transação com recebedor sem saldo, valor R$10.00, pagador CPF 66677788899

2. Os motivos aceitos para MED (Mecanismo Especial de Devolução) são:
   - `FRAUDE_COMPROVADA`
   - `FALHA_OPERACIONAL_BANCO`
   - `COBRANCA_INDEVIDA`

3. O prazo máximo para solicitação de devolução é de 79 dias.