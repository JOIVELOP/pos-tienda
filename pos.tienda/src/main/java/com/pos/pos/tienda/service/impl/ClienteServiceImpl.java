package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.ClienteDTO;
import com.pos.pos.tienda.model.Cliente;
import com.pos.pos.tienda.repository.ClienteRepository;
import com.pos.pos.tienda.service.ClienteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    private static final String CLIENTE_NO_ENCONTRADO = "Cliente no encontrado: ";

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public List<ClienteDTO> findAll() {
        return clienteRepository.findByActivoTrue()
                .stream().map(this::toDTO).toList();
    }

    @Override
    public ClienteDTO findById(Long id) {
        return toDTO(clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CLIENTE_NO_ENCONTRADO + id)));
    }

    @Override
    public List<ClienteDTO> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public ClienteDTO save(ClienteDTO dto) {
        if (dto.getRfc() != null && clienteRepository.existsByRfc(dto.getRfc())) {
            throw new BusinessException("Ya existe un cliente con ese RFC");
        }
        return toDTO(clienteRepository.save(toEntity(dto)));
    }

    @Override
    public ClienteDTO update(Long id, ClienteDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CLIENTE_NO_ENCONTRADO + id));
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setEmail(dto.getEmail());
        cliente.setRfc(dto.getRfc());
        cliente.setRazonSocial(dto.getRazonSocial());
        cliente.setRegimenFiscal(dto.getRegimenFiscal());
        cliente.setActivo(dto.getActivo());
        return toDTO(clienteRepository.save(cliente));
    }

    @Override
    public void delete(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CLIENTE_NO_ENCONTRADO + id));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private ClienteDTO toDTO(Cliente c) {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setTelefono(c.getTelefono());
        dto.setEmail(c.getEmail());
        dto.setRfc(c.getRfc());
        dto.setRazonSocial(c.getRazonSocial());
        dto.setRegimenFiscal(c.getRegimenFiscal());
        dto.setActivo(c.getActivo());
        return dto;
    }

    private Cliente toEntity(ClienteDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setEmail(dto.getEmail());
        cliente.setRfc(dto.getRfc());
        cliente.setRazonSocial(dto.getRazonSocial());
        cliente.setRegimenFiscal(dto.getRegimenFiscal());
        cliente.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
        return cliente;
    }
}
